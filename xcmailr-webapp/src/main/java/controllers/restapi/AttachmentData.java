package controllers.restapi;

import java.io.IOException;

import javax.activation.DataSource;

/**
 * The data object that represents the details of a mail attachment.
 */
public class AttachmentData
{
    public final String name;

    public final String contentType;

    public final int size;

    public AttachmentData(DataSource attachment) throws IOException
    {
        this.name = attachment.getName();
        this.contentType = attachment.getContentType();
        this.size = attachment.getInputStream().available();
    }
}
