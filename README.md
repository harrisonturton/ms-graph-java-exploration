## Teams integration local server

This is a super bare-bones Spring Boot server that exposes some routes on
`localhost:8080`. I've been using it to play around with the Microsoft Graph API
authentication/authorization flows.

If you visit `localhost:8080/connect`, it will redirect you into the oAuth
flow. It completes this successfully and returns an access token.

However, I haven't figured out how to turn this access token into an
authenticated instance of the Microsoft Graph SDK client.

### Configuration

There are some important credentials into the `application.properties` file:

```
teams.client.client.id=CLIENT_ID
teams.client.client.secret=CLIENT_SECRET
teams.client.client.secret.id=CLIENT_SECRET_ID
teams.client.tenant.id=TENANT_ID
```

These are private, so I haven't committed them to the repo, but I can share
them privately for others to run this.

These are taken from the [Azure Active Directory](https://portal.azure.com/#home)
page and from the [Developer portal](https://dev.teams.microsoft.com/home).
