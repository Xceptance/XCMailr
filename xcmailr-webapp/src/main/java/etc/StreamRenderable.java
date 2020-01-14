package etc;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import ninja.Context;
import ninja.Renderable;
import ninja.Result;
import ninja.utils.ResponseStreams;

/**
 * A {@link Renderable} implementation that reads its content from a stream. Useful to generate binary responses, such
 * as images or PDF.
 */
public class StreamRenderable implements Renderable
{
    private final InputStream stream;

    private final String contentType;

    public StreamRenderable(final InputStream stream)
    {
        this(stream, null);
    }

    public StreamRenderable(final InputStream stream, final String contentType)
    {
        this.stream = stream;
        this.contentType = contentType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(final Context context, final Result result)
    {
        if (StringUtils.isNotBlank(contentType))
        {
            result.contentType(contentType);
        }

        final ResponseStreams responseStreams = context.finalizeHeaders(result);

        try (InputStream stream = this.stream)
        {
            IOUtils.copy(stream, responseStreams.getOutputStream());
        }
        catch (final IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
