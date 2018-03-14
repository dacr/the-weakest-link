# The Weakest Link challenge

Let’s discover the distributed tracing system named Jaeger (https://github.com/jaegertracing/jaeger)
through a game inspired from "The Weakest Link" (TV game show).

Interested people can join the game, the only requirement is to have a laptop.
During the session, we will try to interconnect our computers to build the longest
chain(s) each of us completing a story by only knowing his nearest neighbor part.

Once the chain is up, we’ll send the initial HTTP request (JSON POST), and thanks to the Jaeger
User interface every-body will be able to take a look to the global trace and find out who was the
current weakest link ! (with high response times or errors)

The sound track : https://www.youtube.com/watch?v=VJnm43i6Rr4 

---

## Introduction

This small challenge will also show your various techs & tools in action :
+ [akka-http web framework](https://doc.akka.io/docs/akka-http/current/index.html)
+ [Kamon monitoring toolkit](http://kamon.io/documentation/get-started/)
+ [Jaeger distributed tracing](https://jaeger.readthedocs.io/en/latest/)
+ [Prometheus](https://prometheus.io/)
+ [Grafana](https://grafana.com/)
+ [Scala language](https://www.scala-lang.org/)
+ [Sbt build tool](https://www.scala-sbt.org/)
+ [better-files](https://github.com/pathikrit/better-files), 
  [json4s](http://json4s.org/),
  [logback](https://logback.qos.ch/),
  [logstash-logback-encoder](https://github.com/logstash/logstash-logback-encoder),
  ...

---

## Instructions
### Step 0 - Prerequisites

* Check you internet access
  + Connect to the provided 4G router
* Install SBT
  + https://www.scala-sbt.org/
* Get the `the-weakest-link` application source code
  + from github : `git clone https://github.com/dacr/the-weakest-link.git`
* Check your java release, a not too old 1.8 jvm is required

---

## Instructions
### Step 1 - Configure

* Edit `src/main/resources/application.conf` (**Check TODO comments**)
  + Change `kamon.environment.service` to your personal ID
  + Change jaeger IP from `localhost` to the IP we gave you
* Edit `src/main/scala/dummy/Dummy.scala` (**Check TODO comment**)
  + Change `myNeighborIp` localhost to the IP of your nearest neighbor 

---

## Instructions
### Step 2 - Start the app

* Starts the http asynchronous service :
    + `sbt run`
* Check if it works fine
    + `curl -s http://localhost:8080`
        - should return `{"status":"OK"}`

---

## Instructions
### Step 3 - Customize the story

* Check the connectivity with your neighbor, and get its story part
    ```bash
     curl -s http://localhost:8080/asknext
    ```

* Build your own story part by continuing what your neighbor has written
  + Edit the file `myStoryPart.txt`
    - automatically generated on service first start
    - prefilled with a message : `undefined-MyHostName`
  + Of course you will have to wait for your neighbor to have his file
    updated with his own story part.
* Check your own story part :
    ```bash
     curl -s http://localhost:8080/ask
    ```

---

## Instructions
### Step 4 - Who is the weakest Link ?

* I'll send the first http POST request
* **Let's read the global story :)**
* Browse to Jaeger user interface to answer :
  + Who is the slowest link ?
  + If some error was encountered, who is the guilty link ?

---

## Instructions
### Step 5 - Let's monitor everything

* Give me your IP
  + for me to setup my prometheus instance
* Browse to Grafana user interface
* Let's build a dashboard

---

## Notes for the challenge administrators
### Start Jaeger

```
docker run -d \
   --name jaeger \
   -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 \
   -p 0.0.0.0:5775:5775/udp \
   -p 0.0.0.0:6831:6831/udp \
   -p 0.0.0.0:6832:6832/udp \
   -p 0.0.0.0:5778:5778 \
   -p 0.0.0.0:16686:16686 \
   -p 0.0.0.0:14268:14268 \
   -p 0.0.0.0:9411:9411 \
   jaegertracing/all-in-one:latest
```

Browse to IP:16686 to reach the Jaeger user interface.

---

## Notes for the challenge administrators
### Start Prometheus & grafana

A simple prometheus.yml file for this app is in the git repository
root directory. **TAKE CARE WITH THE IP INSIDE THIS FILE**

```bash
docker run -d -p 0.0.0.0:9090:9090 \
   --name kapromotheus \
   -v $PWD/prometheus.yml:/etc/prometheus/prometheus.yml \
   prom/prometheus

docker run -d --name=grafana -p 0.0.0.0:3000:3000 grafana/grafana
```

---

## Notes for the challenge administrators
### Prometheus/grafana queries example

Some prometheus query examples :
```
rate(akka_http_server_open_connections_sum[5m])
rate(akka_http_server_active_requests_sum[5m])
rate(akka_system_active_actors_sum[5m])
```

---

## Notes for the challenge administrators
### Prometheus/grafana template

Template variable configuration :
```
Variable Name : InstanceName
Query : label_values(akka_http_server_open_connections_sum, instance)
Include All option : Enabled
```

Grafana graph configuration with template
```
A: rate(akka_http_server_open_connections_sum{instance=~"$InstanceName"}[5m])
Legend Format : {{ instance }}
```

## Notes for the challenge administrators
### Full chain request

```bash
curl -s \
  -H 'content-type: application/json'   \
  -d '{"maxDepth":20}' \
  http://localhost:8080/chain | jq
```

```bash
curl -s \
  -H 'content-type: application/json'   \
  -d '{"maxDepth":20, "failDepth":2}' \
  http://localhost:8080/chain | jq
```
