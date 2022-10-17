/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.kkukie.jrtsp_gw.media.core.component.audio;

import org.kkukie.jrtsp_gw.media.core.component.AbstractSink;
import org.kkukie.jrtsp_gw.media.core.component.AbstractSource;
import org.kkukie.jrtsp_gw.media.core.concurrent.ConcurrentCyclicFIFO;
import org.kkukie.jrtsp_gw.media.rtp.RtpPacket;
import org.kkukie.jrtsp_gw.media.core.scheduler.PriorityQueueScheduler;
import org.kkukie.jrtsp_gw.media.core.spi.memory.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements output for compound components.
 * 
 * @author Yulian Oifa
 */
public class AudioOutput extends AbstractSource {

	private static final long serialVersionUID = -5988244809612104056L;

	private static final Logger logger = LoggerFactory.getLogger(AudioOutput.class);

	private int outputId;
	private ConcurrentCyclicFIFO<Frame> buffer = new ConcurrentCyclicFIFO<Frame>();

	/**
	 * Creates new instance with default name.
	 */
	public AudioOutput(PriorityQueueScheduler scheduler, int outputId) {
		super("compound.output", scheduler, PriorityQueueScheduler.OUTPUT_QUEUE);
		this.outputId = outputId;
	}

	public int getOutputId() {
		return outputId;
	}

	public void join(AbstractSink sink) {
		connect(sink);
	}

	public void unjoin() {
		disconnect();
	}

	@Override
	public Frame evolve(long timestamp) {
		return buffer.poll();
	}

	@Override
	public void stop() {
		while (buffer.size() > 0) {
			Frame frame = buffer.poll();
			if(frame != null) {
			    frame.recycle();
			}
		}
		super.stop();
	}

	public void resetBuffer() {
		while (buffer.size() > 0) {
			buffer.poll().recycle();
		}
	}

	public void offer(Frame frame) {
		if (buffer.size() > 1) {
			buffer.poll().recycle();
		}

		RtpPacket rtpPacket = new RtpPacket(RtpPacket.RTP_PACKET_MAX_SIZE, true);
		rtpPacket.wrap(frame.getData());
		logger.debug("AudioOutput: rtp={}", rtpPacket.toString());
		buffer.offer(frame);
	}
}
