package etc;

import java.io.IOException;

public class SizeLimitExceededException extends IOException
{
    static final long serialVersionUID = -2495864848105342730L;

    public SizeLimitExceededException()
    {
        super();
    }

    public SizeLimitExceededException(String arg0, Throwable arg1)
    {
        super(arg0, arg1);
    }

    public SizeLimitExceededException(String arg0)
    {
        super(arg0);
    }

    public SizeLimitExceededException(Throwable arg0)
    {
        super(arg0);
    }

}