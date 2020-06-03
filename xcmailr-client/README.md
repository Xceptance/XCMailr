# XCMailr Client

The XCMailr client library is an easy-to-use Java library that simplifies using the REST API of [XCMailr](https://github.com/Xceptance/XCMailr) in test projects. The library abstracts away almost all details of the underlying protocol.


## Adding the Client Library to Your Project 

Add the XCMailr client library as another dependency to your project:

```xml
<dependency>
    <groupId>com.xceptance</groupId>
    <artifactId>xcmailr-client</artifactId>
    <version>2.2.0</version>
</dependency>
```

## Usage Examples

### Initializing the Client

Before you can use the REST API you need to create a client instance and initialize it with the XCMailr URL and your API token. To get such a token, log in to XCMailr and create an API token for your account.

```java
// initialize the client
String xcmailrUrl = "https://xcmailr.example.org/";
String apiToken = "111122223333444455556666";
XCMailrClient client = new XCMailrClient(xcmailrUrl, apiToken);
```

### Managing Mailboxes

Before we can receive mails, we have to create a mailbox at XCMailr.

```java
// create a mailbox
String mailboxAddress = "john.doe@xcmailr.test";
String minutesActive = 10;
String forwardEnabled = false;
Mailbox mailbox = client.mailboxes().createMailbox(mailboxAddress, minutesActive, forwardEnabled);

// delete a mailbox
client.mailboxes().deleteMailbox(mailboxAddress);
```

### Retrieving and Validating Mails and Mail Attachments

Once we have created a mailbox and mails have been sent to it, we can retrieve those mails via the API.

```java
// retrieve mails
String subjectPattern = "Order Confirmation";
List<Mail> mails = client.mails().listMails(mailboxAddress, new MailFilterOptions().lastMatchOnly(true).subjectPattern(subjectPattern));

// pick a mail
Mail mail = mails.get(0);

// check mail content
// ...

// download attachment
InputStream is = client.mails().openAttachment(mail.id, attachmentName);
byte[] data = is.readAllBytes();

// check attachment data
// ...

// finally delete the mail
client.mails().deleteMail(mail.id);
```