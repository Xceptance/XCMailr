package etc;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.activation.DataSource;

import org.apache.commons.io.IOUtils;

public class AttachmentEntry
{
    public AttachmentEntry(DataSource attachment) throws IOException
    {
        this.name = attachment.getName();
        this.contentType = attachment.getContentType();
        this.content = IOUtils.toString(attachment.getInputStream(), Charset.defaultCharset());
    }

    public String name;

    public String contentType;

    public String content;
}
