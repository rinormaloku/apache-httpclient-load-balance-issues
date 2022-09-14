## Imbalanced load when using Apache HTTP Client

Client workloads without Istio sidecars cause imbalanced load when using Apache HTTP Client. This happens because the clients keep the connection alive to the server proxy. This happens because [when the Server doesn't specify the keep-alive header the connection is retained indefinitely](https://hc.apache.org/httpcomponents-client-4.5.x/current/tutorial/html/connmgmt.html#:~:text=If%20the%20Keep%2DAlive%20header%20is%20not%20present%20in%20the%20response%2C%20HttpClient%20assumes%20the%20connection%20can%20be%20kept%20alive%20indefinitely.). The envoy proxy removes the Keep-Alive header, thus the clients without the sidecar retain a connection indefinitely.

To test that run the following commands.

Make sure to have istio installed in the cluster and prometheus to visualize your findings.
```
istioctl install -y --set profile=demo
kubectl apply -f istio-addons
```

Create a namespace, and apply the client workload. **THE SIDECAR IS NOT INJECTED**
```
# create ns
kubectl create ns client

kubectl -n client apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: java-http-client
  name: java-http-client
spec:
  replicas: 1
  selector:
    matchLabels:
      app: java-http-client
  template:
    metadata:
      labels:
        app: java-http-client
    spec:
      containers:
      - image: rinormaloku/java-http-client
        imagePullPolicy: IfNotPresent
        name: java-http-client
        ports:
        - containerPort: 8080
          name: http
          protocol: TCP
        env:
        - name: URL
          value: http://java-request-bin.server:8080/
        - name: WITH_TIMEOUT
          value: "false"
---
apiVersion: v1
kind: Service
metadata:
  labels:
    app: java-http-client
  name: java-http-client
spec:
  ports:
  - name: http
    port: 8080
    protocol: TCP
    targetPort: 8080
  selector:
    app: java-http-client
---
EOF
```

Create a namespace and the deployment for the server workloads **SIDECAR IS INJECTED**

```
kubectl create ns server
kubectl label namespace server istio-injection=enabled

kubectl -n server apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: java-request-bin
  name: java-request-bin
spec:
  replicas: 1
  selector:
    matchLabels:
      app: java-request-bin
  template:
    metadata:
      labels:
        app: java-request-bin
    spec:
      containers:
      - image: rinormaloku/java-request-bin
        imagePullPolicy: IfNotPresent
        name: java-request-bin
        ports:
        - containerPort: 8080
          name: http
          protocol: TCP
        env:
        - name: URL
          value: http://java-request-bin.server:8080/
        - name: WITH_TIMEOUT
          value: "true" 
---
apiVersion: v1
kind: Service
metadata:
  labels:
    app: java-request-bin
  name: java-request-bin
spec:
  ports:
  - name: http
    port: 8080
    protocol: TCP
    targetPort: 8080
  selector:
    app: java-request-bin
---
EOF
```

## The test

Scale up the client to 10 replicas and wait for all to be ready and running
```
kubectl -n client scale deploy/java-http-client --replicas 10

sleep 5; kubectl -n client wait po --for condition=Ready --timeout -1s --all
```

Next let's scale out the server side.

```
kubectl -n server scale deploy/java-request-bin --replicas 4

kubectl -n server wait po --for condition=Ready --timeout -1s --all
```

Open prometheus
```
istioctl dashboard prometheus
```
Check the workloads that receive the traffic with the query below:
```
sum(irate(
    istio_requests_total{reporter="destination", app="java-request-bin"}[1m])) 
by (kubernetes_pod_name)
```

If you delete the clients the load will slightly balance, because new connections are formed.
```
kubectl -n client delete pods --all
kubectl -n client wait po --for condition=Ready --timeout -1s --all
```

But that is not a solution,


## Solution: Will a sidecar fix this?

Reset server to 1 replica:
```
kubectl -n server scale deploy/java-request-bin --replicas 1
```

Label the client side for sidecar injection **SIDECAR IS INJECTED TO CLIENT AS WELL**

```bash
kubectl label namespace client istio-injection=enabled
sleep 5; kubectl -n client scale deploy/java-http-client --replicas 0

# scale back up to 1 this will inject the sidecar as we enabled sidecar injection
kubectl -n client scale deploy/java-http-client --replicas 1
sleep 5; kubectl -n client wait po --for condition=Ready --timeout -1s --all
```

Scale out the client

```
kubectl -n client scale deploy/java-http-client --replicas 10

sleep 5; kubectl -n client wait po --for condition=Ready --timeout -1s --all
```

Scale out the server:
```
kubectl -n server scale deploy/java-request-bin --replicas 4
kubectl -n server wait po --for condition=Ready --timeout -1s --all
```

If you check prometheus you will see that the load got immediately balanced.
