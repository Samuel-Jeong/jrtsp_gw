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

package org.kkukie.jrtsp_gw.media.core.stream.rtp.format;

import org.kkukie.jrtsp_gw.session.media.format.Format;
import org.kkukie.jrtsp_gw.session.media.format.Formats;

import java.util.ArrayList;

/**
 * Implements RTP formats collection with fast search.
 * <p>
 * We assume that RTP formats collection varies slow.
 *
 * @author kulikov
 */
public class RTPFormats {

    //the default size of this collection
    private static final int SIZE = 10;

    //backing array
    private final ArrayList<RTPFormat> rtpFormats;

    private final Formats formats = new Formats();

    private int cursor;

    /**
     * Creates new format collection with default size.
     */
    public RTPFormats () {
        this.rtpFormats = new ArrayList<>(SIZE);
    }

    /**
     * Creates new formats collection with specified size
     *
     * @param size the size of collection to be created.
     */
    public RTPFormats (int size) {
        this.rtpFormats = new ArrayList<>(size);
    }

    public int getLen () {
        return this.rtpFormats.size();
    }

    public void add (RTPFormat rtpFormat) {
        rtpFormats.add(rtpFormat);
        formats.add(rtpFormat.getFormat());
    }

    public void add (RTPFormats fmts) {
        for (int i = 0; i < fmts.rtpFormats.size(); i++) {
            rtpFormats.add(fmts.rtpFormats.get(i));
            formats.add(fmts.rtpFormats.get(i).getFormat());
        }
    }

    public void remove (RTPFormat rtpFormat) {
        int pos = -1;
        for (RTPFormat format : rtpFormats) {
            pos++;
            if (format.getID() == rtpFormat.getID()) break;
        }

        if (pos == -1) {
            throw new IllegalArgumentException("Unknown format " + rtpFormat);
        }

        rtpFormats.remove(pos);
        formats.remove(rtpFormat.getFormat());
    }

    public void clean () {
        rtpFormats.clear();
        formats.clean();
        cursor = 0;
    }

    public int size () {
        return rtpFormats.size();
    }

    public RTPFormat getRTPFormat (int payload) {
        for (RTPFormat rtpFormat : rtpFormats) {
            if (rtpFormat.getID() == payload) return rtpFormat;
        }
        return null;
    }

    public RTPFormat getRTPFormat (String name) {
        for (final RTPFormat rtpFormat : rtpFormats) {
            if (rtpFormat.getFormat().getName().toString().equalsIgnoreCase(name)) {
                return rtpFormat;
            }
        }
        return null;
    }

    public RTPFormat getRTPFormat (Format format) {
        for (RTPFormat rtpFormat : rtpFormats) {
            if (rtpFormat.getFormat().matches(format)) return rtpFormat;
        }
        return null;
    }

    public RTPFormat[] toArray () {
        RTPFormat[] fmts = new RTPFormat[rtpFormats.size()];
        return rtpFormats.toArray(fmts);
    }

    public Formats getFormats () {
        return formats;
    }

    public RTPFormat find (int p) {
        for (RTPFormat rtpFormat : rtpFormats) {
            if (rtpFormat.getID() == p) {
                return rtpFormat;
            }
        }
        return null;
    }

    public boolean contains (int p) {
        return this.find(p) != null;
    }

    public boolean contains (Format fmt) {
        for (RTPFormat rtpFormat : rtpFormats) {
            if (rtpFormat.getFormat().matches(fmt)) {
                return true;
            }
        }
        return false;
    }

    public RTPFormat find (Format fmt) {
        for (RTPFormat rtpFormat : rtpFormats) {
            if (rtpFormat.getFormat().matches(fmt)) {
                return rtpFormat;
            }
        }
        return null;
    }

    public boolean isEmpty () {
        return rtpFormats.isEmpty();
    }

    public void rewind () {
        cursor = 0;
    }

    public boolean hasMore () {
        return cursor != rtpFormats.size();
    }

    public RTPFormat next () {
        return rtpFormats.get(cursor++);
    }

    public void intersection (RTPFormats other, RTPFormats res) {
        for (int i = 0; i < other.size(); i++) {
            RTPFormat supportedFormat = other.rtpFormats.get(i);
            for (RTPFormat offeredFormat : this.rtpFormats) {
                if (supportedFormat.getFormat().matches(offeredFormat.getFormat())) {
                    // Add offered (instead of supported) format for DTMF dynamic payload
                    res.add(supportedFormat);
                    break;
                }
            }
        }
    }

    @Override
    public String toString () {
        StringBuilder buffer = new StringBuilder();
        buffer.append("RTPFormats{");

        for (int i = 0; i < rtpFormats.size(); i++) {
            buffer.append(rtpFormats.get(i));
            if (i != rtpFormats.size() - 1) buffer.append(",");
        }

        buffer.append("}");
        return buffer.toString();
    }

}
