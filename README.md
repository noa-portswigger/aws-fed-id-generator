# A aws-fed-id token generator in java

The code in this project generates tokens that can be used to authenticate the holder of the token as having
a specific AWS IAM Role. 

The credentials needed to generate the token are using the regular method of the AWS SDK
to get credentials from a variety of sources such as environment variables, credentials stored in files in 
`$HOME/.aws` or through the IAM Roles for service accounts (IRSA) mechanism.

The format of this token is the same as the format used by the Google Cloud Platform (GCP) federated identity mechanism,
as documented [here](https://cloud.google.com/iam/docs/workload-identity-federation-with-other-clouds#advanced_scenarios). 
This means that if the audience parameter is set to the value expected by GCP, this code can be used as-is to get
a token associated with an GCP identity.

However, there is nothing specific to Google about this authentication mechanism. It can also be used to prove
your identity with other third party services that validate the token the same way that GCP would validate it,
and can use the AWS IAM Role to grant access to other resources.

## Usage

To use this code, instantiate a TokenGenerator. The default constructor will be fine for most use cases, using the
default AWS credentials provider chain and generating tokens to be verified by the global AWS STS endpoint. If you know
that the validation will be done in a single region, you might achieve higher availability and lower latency by specifying
the region in the constructor.

My recommendation would be to add the token to the HTTP Authorization header with the mechanism identifier `aws-fed-id`, as
in the following example. Replace `example.com` with the domain of the service that be validating the token.

```
TokenGenerator generator = new TokenGenerator();
request.headers(headers -> headers.add(
    "Authorization", "aws-fed-id " + generator.generate("example.com")
));
```