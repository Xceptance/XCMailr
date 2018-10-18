package etc;

public class ApiToken
{
    public String token;

    public long expirationTimestamp;

    public ApiToken(String token, long expirationTimestamp)
    {
        this.token = token;
        this.expirationTimestamp = expirationTimestamp;
    }
}
