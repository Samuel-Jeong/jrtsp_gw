package org.kkukie.jrtsp_gw.media.core.manager;

import lombok.extern.slf4j.Slf4j;
import org.kkukie.jrtsp_gw.media.core.scheduler.ServiceScheduler;
import org.kkukie.jrtsp_gw.media.core.model.DataChannel;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 1) 등록된 UDP 채널을 모니터링하면서 해당 채널의 패킷 수신 기능을 트리거링한다.
 * 2) 현재는 채널 수신(읽기, SelectionKey.OP_READ)만 트리거링한다. (포로그램 목적에 부합)
 * 3) 요청된 호(CallId가 배정된 WebSocketService)당 하나만 존재한다.
 */

@Slf4j
public class PacketSelector {

    private final String conferenceId;

    private volatile boolean active;
    private final Object LOCK;
    private final List<Selector> selectors;
    private final List<PollTask> pollTasks;
    private final List<Future<?>> pollTaskFutures;
    private final AtomicInteger currSelectorIndex;
    private final ServiceScheduler scheduler = new ServiceScheduler();

    public PacketSelector(String conferenceId) {
        this.conferenceId = conferenceId;
        
        LOCK = new Object();
        active = false;
        selectors = new ArrayList<>(ServiceScheduler.POOL_SIZE);
        pollTasks = new ArrayList<>(ServiceScheduler.POOL_SIZE);
        pollTaskFutures = new ArrayList<>(ServiceScheduler.POOL_SIZE);
        currSelectorIndex = new AtomicInteger(0);

        log.debug("|PacketSelector({})| ServiceScheduler.POOL_SIZE = [ {} ]", conferenceId, ServiceScheduler.POOL_SIZE);
    }

    public void registerChannel(DatagramChannel datagramChannel, DataChannel dataChannel) {
        try {
            int index = currSelectorIndex.getAndIncrement();
            if (index >= selectors.size()) {
                currSelectorIndex.set(index % selectors.size());
                index = currSelectorIndex.get();
            }
            SelectionKey key = datagramChannel.register(
                    selectors.get(index),
                    SelectionKey.OP_READ
            );
            key.attach(dataChannel);
            log.debug("|PacketSelector({})| Success to register the channel. (localAddress={}, isBlocking={})",
                    conferenceId, datagramChannel.getLocalAddress(), datagramChannel.isBlocking()
            );
        } catch (Exception e) {
            log.warn("|PacketSelector({})| Fail to register the datagram channel.", conferenceId, e);
        }
    }

    public void unregisterChannel(DataChannel dataChannel) {
        try {
            SelectionKey selectionKey = dataChannel.getSelectionKey();
            if (selectionKey != null) {
                selectionKey.cancel();
                log.debug("|PacketSelector({})| Success to unregister the channel.", conferenceId);
            }
        } catch (Exception e) {
            log.warn("|PacketSelector({})| Fail to unregister the datagram channel.", conferenceId, e);
        }
    }

    private void generateTasks() throws IOException {
        for (int i = 0; i < ServiceScheduler.POOL_SIZE; i++) {
            selectors.add(SelectorProvider.provider().openSelector());
            PollTask pollTask = new PollTask(i, selectors.get(i));
            pollTasks.add(pollTask);
            ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(pollTask, 0L, 1L, TimeUnit.MILLISECONDS);
            pollTaskFutures.add(future);
        }
    }

    private void stopTasks() {
        for (Future<?> future : pollTaskFutures) {
            future.cancel(false);
        }
        pollTaskFutures.clear();
    }

    private void closeSelectors() {
        for (int i = 0; i < selectors.size(); i++) {
            Selector selector = selectors.get(i);
            if (selector != null && selector.isOpen()) {
                try {
                    selector.close();
                } catch (Exception e) {
                    log.warn("|PacketSelector({})| Could not close selector({}).", conferenceId, i, e);
                }
            }
        }
    }

    private void cleanResources() {
        pollTasks.clear();
        selectors.clear();
    }

    public void start() {
        synchronized (LOCK) {
            if (!active) {
                active = true;
                try {
                    scheduler.start();
                    generateTasks();
                    log.debug("|PacketSelector({})| Started.", conferenceId);
                } catch (IOException e) {
                    log.warn("|PacketSelector({})| An error occurred while initializing the polling tasks.", conferenceId, e);
                    stop();
                }
            }
        }
    }

    public void stop() {
        synchronized (LOCK) {
            if (active) {
                active = false;
                log.debug("|PacketSelector({})| Stopping...", conferenceId);
                stopTasks();
                closeSelectors();
                cleanResources();
                scheduler.stop();
                log.debug("|PacketSelector({})| Stopped.", conferenceId);
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////

    private class PollTask implements Runnable {

        private final int id;
        private final Selector localSelector;

        public PollTask(int id, Selector selector) {
            this.id = id;
            localSelector = selector;
        }

        @Override
        public void run() {
            if (active) {
                try {
                    // Select channels enabled for reading operation (without blocking!)
                    int selected = localSelector.selectNow();
                    if (selected == 0) {
                        //Thread.yield();
                        return;
                    }
                } catch (IOException e) {
                    log.error("|PacketSelector({})| |PollTask({})| Could not select channels from Selector!", conferenceId, id);
                }

                // Iterate over selected channels
                Iterator<SelectionKey> it = localSelector.selectedKeys().iterator();
                while (it.hasNext() && active) {
                    SelectionKey key = it.next();
                    it.remove();

                    // Get references to channel and associated RTP socket
                    DatagramChannel datagramChannel = (DatagramChannel) key.channel();
                    Object attachment = key.attachment();
                    if (attachment == null) { continue; }

                    try {
                        if (attachment instanceof DataChannel) {
                            DataChannel channel = (DataChannel) attachment;
                            if (datagramChannel.isOpen()) {
                                if (key.isValid()) {
                                    channel.receive();
                                    if (channel.hasPendingData()) {
                                        channel.send();
                                    }
                                }
                            } else {
                                channel.close();
                            }
                        } else {
                            log.warn("|PacketSelector({})| |PollTask({})| Not defined the attachment.", conferenceId, id);
                        }
                    } catch (Exception e) {
                        log.error("|PacketSelector({})| |PollTask({})| An unexpected problem occurred while reading from channel.", conferenceId, id, e);
                    }
                }

                localSelector.selectedKeys().clear();
            } else {
                Thread.yield();
            }
        }
    }

}
