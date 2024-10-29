package io.sentry;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/*
 * Utility class for faster id generation for SentryId, SpanId, and unique filenames uses throughout
 * the SDK It uses our vendored Random class instead of SecureRandom for improved performance It
 * directly creates a correctly formatted String to be used as IDs in the Sentry context.
 *
 * <p>Id generation is sped up by 4 to 10 times based on the underlying java version.
 *
 * <p>Based on the work of <a href="https://github.com/jchambers/">Jon Chambers</a> <a href="https://github.com/jchambers/fast-uuid">here</a>.
 *
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Jon Chambers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

public final class SentryUUID {

  private static final int SENTRY_UUID_STRING_LENGTH = 32;
  private static final int SENTRY_SPAN_UUID_STRING_LENGTH = 16;

  private static final char[] HEX_DIGITS =
      new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

  private static final long[] HEX_VALUES = new long[128];

  static {
    Arrays.fill(HEX_VALUES, -1);

    HEX_VALUES['0'] = 0x0;
    HEX_VALUES['1'] = 0x1;
    HEX_VALUES['2'] = 0x2;
    HEX_VALUES['3'] = 0x3;
    HEX_VALUES['4'] = 0x4;
    HEX_VALUES['5'] = 0x5;
    HEX_VALUES['6'] = 0x6;
    HEX_VALUES['7'] = 0x7;
    HEX_VALUES['8'] = 0x8;
    HEX_VALUES['9'] = 0x9;

    HEX_VALUES['a'] = 0xa;
    HEX_VALUES['b'] = 0xb;
    HEX_VALUES['c'] = 0xc;
    HEX_VALUES['d'] = 0xd;
    HEX_VALUES['e'] = 0xe;
    HEX_VALUES['f'] = 0xf;

    HEX_VALUES['A'] = 0xa;
    HEX_VALUES['B'] = 0xb;
    HEX_VALUES['C'] = 0xc;
    HEX_VALUES['D'] = 0xd;
    HEX_VALUES['E'] = 0xe;
    HEX_VALUES['F'] = 0xf;
  }

  private SentryUUID() {
    // A private constructor prevents callers from accidentally instantiating FastUUID objects
  }

  public static String generateSentryId() {
    return toSentryIdString(SentryUUID.randomUUID());
  }

  public static String generateSpanId() {
    return toSentrySpanIdString(SentryUUID.randomHalfLengthUUID());
  }

  public static String toSentryIdString(final UUID uuid) {

    final long mostSignificantBits = uuid.getMostSignificantBits();
    final long leastSignificantBits = uuid.getLeastSignificantBits();

    return toSentryIdString(mostSignificantBits, leastSignificantBits);
  }

  public static String toSentryIdString(long mostSignificantBits, long leastSignificantBits) {
    final char[] uuidChars = new char[SENTRY_UUID_STRING_LENGTH];

    fillMostSignificantBits(uuidChars, mostSignificantBits);

    uuidChars[16] = HEX_DIGITS[(int) ((leastSignificantBits & 0xf000000000000000L) >>> 60)];
    uuidChars[17] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0f00000000000000L) >>> 56)];
    uuidChars[18] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00f0000000000000L) >>> 52)];
    uuidChars[19] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000f000000000000L) >>> 48)];
    uuidChars[20] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000f00000000000L) >>> 44)];
    uuidChars[21] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000f0000000000L) >>> 40)];
    uuidChars[22] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000000f000000000L) >>> 36)];
    uuidChars[23] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000000f00000000L) >>> 32)];
    uuidChars[24] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000000f0000000L) >>> 28)];
    uuidChars[25] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000000000f000000L) >>> 24)];
    uuidChars[26] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000000000f00000L) >>> 20)];
    uuidChars[27] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000000000f0000L) >>> 16)];
    uuidChars[28] = HEX_DIGITS[(int) ((leastSignificantBits & 0x000000000000f000L) >>> 12)];
    uuidChars[29] = HEX_DIGITS[(int) ((leastSignificantBits & 0x0000000000000f00L) >>> 8)];
    uuidChars[30] = HEX_DIGITS[(int) ((leastSignificantBits & 0x00000000000000f0L) >>> 4)];
    uuidChars[31] = HEX_DIGITS[(int) (leastSignificantBits & 0x000000000000000fL)];

    return new String(uuidChars);
  }

  public static String toSentrySpanIdString(final UUID uuid) {

    final long mostSignificantBits = uuid.getMostSignificantBits();
    return toSentrySpanIdString(mostSignificantBits);
  }

  public static String toSentrySpanIdString(long mostSignificantBits) {
    final char[] uuidChars = new char[SENTRY_SPAN_UUID_STRING_LENGTH];

    fillMostSignificantBits(uuidChars, mostSignificantBits);

    return new String(uuidChars);
  }

  private static void fillMostSignificantBits(
      final char[] uuidChars, final long mostSignificantBits) {
    uuidChars[0] = HEX_DIGITS[(int) ((mostSignificantBits & 0xf000000000000000L) >>> 60)];
    uuidChars[1] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0f00000000000000L) >>> 56)];
    uuidChars[2] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00f0000000000000L) >>> 52)];
    uuidChars[3] = HEX_DIGITS[(int) ((mostSignificantBits & 0x000f000000000000L) >>> 48)];
    uuidChars[4] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000f00000000000L) >>> 44)];
    uuidChars[5] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000f0000000000L) >>> 40)];
    uuidChars[6] = HEX_DIGITS[(int) ((mostSignificantBits & 0x000000f000000000L) >>> 36)];
    uuidChars[7] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000000f00000000L) >>> 32)];
    uuidChars[8] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000000f0000000L) >>> 28)];
    uuidChars[9] = HEX_DIGITS[(int) ((mostSignificantBits & 0x000000000f000000L) >>> 24)];
    uuidChars[10] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000000000f00000L) >>> 20)];
    uuidChars[11] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000000000f0000L) >>> 16)];
    uuidChars[12] = HEX_DIGITS[(int) ((mostSignificantBits & 0x000000000000f000L) >>> 12)];
    uuidChars[13] = HEX_DIGITS[(int) ((mostSignificantBits & 0x0000000000000f00L) >>> 8)];
    uuidChars[14] = HEX_DIGITS[(int) ((mostSignificantBits & 0x00000000000000f0L) >>> 4)];
    uuidChars[15] = HEX_DIGITS[(int) (mostSignificantBits & 0x000000000000000fL)];
  }

  static long getHexValueForChar(final char c) {
    try {
      if (HEX_VALUES[c] < 0) {
        throw new IllegalArgumentException("Illegal hexadecimal digit: " + c);
      }
    } catch (final ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Illegal hexadecimal digit: " + c);
    }

    return HEX_VALUES[c];
  }

  @SuppressWarnings("NarrowingCompoundAssignment")
  public static long randomHalfLengthUUID() {
    Random random = SentryUUID.Holder.numberGenerator;
    byte[] randomBytes = new byte[8];
    random.nextBytes(randomBytes);
    randomBytes[6] &= 0x0f; /* clear version        */
    randomBytes[6] |= 0x40; /* set to version 4     */

    long msb = 0;

    for (int i = 0; i < 8; i++) msb = (msb << 8) | (randomBytes[i] & 0xff);

    return msb;
  }

  @SuppressWarnings("NarrowingCompoundAssignment")
  public static UUID randomUUID() {
    Random random = SentryUUID.Holder.numberGenerator;
    byte[] randomBytes = new byte[16];
    random.nextBytes(randomBytes);
    randomBytes[6] &= 0x0f; /* clear version        */
    randomBytes[6] |= 0x40; /* set to version 4     */
    randomBytes[8] &= 0x3f; /* clear variant        */
    randomBytes[8] |= (byte) 0x80; /* set to IETF variant  */

    long msb = 0;
    long lsb = 0;

    for (int i = 0; i < 8; i++) msb = (msb << 8) | (randomBytes[i] & 0xff);

    for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (randomBytes[i] & 0xff);

    return new UUID(msb, lsb);
  }

  @SuppressWarnings("NarrowingCompoundAssignment")
  public static UUID randomUUIDThreadLocal() {
    Random random = ThreadLocalRandom.current();
    byte[] randomBytes = new byte[16];
    random.nextBytes(randomBytes);
    randomBytes[6]  &= 0x0f;  /* clear version        */
    randomBytes[6]  |= 0x40;  /* set to version 4     */
    randomBytes[8]  &= 0x3f;  /* clear variant        */
    randomBytes[8]  |= (byte) 0x80;  /* set to IETF variant  */

    long msb = 0;
    long lsb = 0;

    for (int i=0; i<8; i++)
      msb = (msb << 8) | (randomBytes[i] & 0xff);

    for (int i=8; i<16; i++)
      lsb = (lsb << 8) | (randomBytes[i] & 0xff);

    return new UUID(msb, lsb);
  }

  public static String generateSentryIdThreaded() {
    return toSentryIdString(SentryUUID.randomUUIDThreadLocal());
  }

  private static class Holder {
    static final Random numberGenerator = new Random();

    private Holder() {
    }
  }

}