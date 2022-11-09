package org.kkukie.jrtsp_gw.util;

import java.util.concurrent.ThreadLocalRandom;

public class RandomManager {

    private RandomManager() {}

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    public static long getRandomLong(long bound) {
        return random.nextLong(bound);
    }

    public static long getRandomLong(long start, long end) {
        return random.nextLong(start, end);
    }

    public static int getIntegerLong(int bound) {
        return random.nextInt(bound);
    }

    public static int getIntegerLong(int start, int end) {
        return random.nextInt(start, end);
    }

    public static String getRandomString(int count) {
        return random(count, 0, 0, true, true, null);
    }

    public static String random(int count, int start, int end, boolean letters, boolean numbers, char[] chars) {
        if (count == 0) {
            return "";
        } else if (count < 0) {
            throw new IllegalArgumentException("Requested random string length " + count + " is less than 0.");
        } else {
            if (start == 0 && end == 0) {
                end = 123;
                start = 32;
                if (!letters && !numbers) {
                    start = 0;
                    end = Integer.MAX_VALUE;
                }
            }

            char[] buffer = new char[count];
            int gap = end - start;

            while(true) {
                while(true) {
                    while(count-- != 0) {
                        char ch;
                        if (chars == null) {
                            ch = (char)(random.nextInt(gap) + start);
                        } else {
                            ch = chars[random.nextInt(gap) + start];
                        }

                        if (letters && Character.isLetter(ch) || numbers && Character.isDigit(ch) || !letters && !numbers) {
                            if (ch >= '\udc00' && ch <= '\udfff') {
                                if (count == 0) {
                                    ++count;
                                } else {
                                    buffer[count] = ch;
                                    --count;
                                    buffer[count] = (char)('\ud800' + random.nextInt(128));
                                }
                            } else if (ch >= '\ud800' && ch <= '\udb7f') {
                                if (count == 0) {
                                    ++count;
                                } else {
                                    buffer[count] = (char)('\udc00' + random.nextInt(128));
                                    --count;
                                    buffer[count] = ch;
                                }
                            } else if (ch >= '\udb80' && ch <= '\udbff') {
                                ++count;
                            } else {
                                buffer[count] = ch;
                            }
                        } else {
                            ++count;
                        }
                    }

                    return new String(buffer);
                }
            }
        }
    }

}
