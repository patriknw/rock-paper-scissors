package io.akka.sample.api;

import akka.javasdk.http.HttpResponses;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint()
public class StaticContentEndpoint {

    @Get("/")
    public HttpResponse root() {
        return index();
    }

    @Get("index.html")
    public HttpResponse index() {
        return HttpResponses.staticResource("index.html");
    }

    @Get("/static/**")
    public HttpResponse allTheResources(HttpRequest request) {
        return HttpResponses.staticResource(request, "/static");
    }

}
