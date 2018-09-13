package etc;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

// All credits to https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string/41156#41156
public class TokenGenerator
{
    /**
     * Generate a random string.
     */
    public String nextString()
    {
        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }

    public static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final String lower = upper.toLowerCase(Locale.ROOT);

    public static final String digits = "0123456789";

    public static final String alphanum = upper + lower + digits;

    private final Random random;

    private final char[] symbols;

    private final char[] buf;

    public TokenGenerator(int length, Random random, String symbols)
    {
        if (length < 1)
            throw new IllegalArgumentException();
        if (symbols.length() < 2)
            throw new IllegalArgumentException();
        this.random = Objects.requireNonNull(random);
        this.symbols = symbols.toCharArray();
        this.buf = new char[length];
    }

    /**
     * Create an alphanumeric string generator.
     */
    public TokenGenerator(int length, Random random)
    {
        this(length, random, alphanum);
    }

    /**
     * Create an alphanumeric strings from a secure generator.
     */
    public TokenGenerator(int length)
    {
        this(length, new SecureRandom());
    }

    /**
     * Create session identifiers.
     */
    public TokenGenerator()
    {
        this(21);
    }
}
