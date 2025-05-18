@Value("${vmb.s3fullmessage:true}")
	private boolean s3FullMessageFlag;

	public boolean isS3FullMessageFlag() {
		return s3FullMessageFlag;
	}

	public void setS3FullMessageFlag(boolean s3FullMessageFlag) {
		this.s3FullMessageFlag = s3FullMessageFlag;
	}

Here we can see I set the value as true it is taking from application.yaml but it is not working from there please make sure to the value should be taken from yaml try other ways to pickupo the value easily

s3fullmessage: ${S3FULLMESSAGE:true}


And I'm trying this value config from helm it is not working this is the configuration:

    kub84: 
      npapp:
        REPOSITORY: enmv-docker-np.oneartifactoryci.verizon.com
        VMB_SERVICE_URL: pulsar+ssl://vmb-aws-us-east-1-nonprod.verizon.com:6651
        IMAGE_TAG: S3Fullmessage
        LOG_APPENDER: STDOUT_JSON_PATTERN
        LOG_LEVEL: INFO
        LOG_TRUNCATE: "true"
        S3OVERRIDEAUTH: ucs-tunnel-np.enmv.ebiz.verizon.com
        S3BUCKETKEY: HPOV-TRAP-CONSUMER/COLLECTOR/HPOVTRAP
        S3API_SERVICE_URL: ms-sthreeapi-idnipv4-service.bbtpnj33vzbcucs-y-vz-npapp-enmv.svc.cluster.local:9998
        CONSUMERS:
          HPOV-TRAP:
            NAME: hpov-trap
            ENABLED: true
            SUBSCRIPTIONNAME: ENMV_HPOV_TRAP_SUBSCRIPTION
            REPLICAS: 1
            TOPICURL: persistent://enmv/hpov-alarm/hpov-snmp-trap
            S3JSON: {"mode":"I","s3Logging":"true","persistenceTimestampFlag":"true","retentionType": "","retentionValue": "5","ucgId": "ucgIdEnabled"}
            S3FULLMESSAGE: true


{{- $top := . -}}
{{- $datacenter := include "app.datacenter" $top -}}
{{- $cluster := include "app.cluster" $top -}}
{{- $env := include "app.env" $top -}}
{{- range $consumers_k, $consumers_v := (index $top.Values "kube" $datacenter $cluster $env "CONSUMERS") -}}
{{- $consumer :=   $consumers_k -}}
{{- $consumers_name := (index $top.Values "kube" $datacenter $cluster $env "CONSUMERS" $consumer "NAME") -}}
{{- $replicas := (index $top.Values "kube" $datacenter $cluster $env "CONSUMERS" $consumer "REPLICAS") -}}
{{- $enabled := (index $top.Values "kube" $datacenter $cluster $env "CONSUMERS" $consumer "ENABLED") -}}
{{- $topicurl := (index $top.Values "kube" $datacenter $cluster $env "CONSUMERS" $consumer "TOPICURL") -}}
{{- $s3json := (index $top.Values "kube" $datacenter $cluster $env "CONSUMERS" $consumer "S3JSON") -}}
{{- $s3fullmessage := (index $top.Values "kube" $datacenter $cluster $env "CONSUMERS" $consumer "S3FULLMESSAGE") -}}
{{- $resources:= toYaml (index $top.Values "kube" $datacenter $cluster $env "CONSUMERS" $consumer "RESOURCES") -}}
{{- $resources_default:= toYaml (index $top.Values "resources_default"  $env) -}}
{{- $subscription_name:= toYaml (index $top.Values "kube" $datacenter $cluster $env "CONSUMERS" $consumer "SUBSCRIPTIONNAME") -}}
{{- if $enabled -}}
---

apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ $consumers_name }}-consumer
spec:
  selector:
    matchLabels:
      app: {{ $consumers_name }}-consumer
  replicas: {{ $replicas }}
  revisionHistoryLimit: 0
  template:
    metadata:
      annotations:
        traffic.sidecar.istio.io/excludeOutboundIPRanges: {{ include "app.excludeOutboundIPRanges" $top }}
        traffic.sidecar.istio.io/excludeOutboundPorts: {{ include "app.excludeOutboundPorts" $top }}
        traffic.sidecar.istio.io/excludeInboundPorts: {{ include "app.excludeInboundPorts" $top }}
      labels:
        app: {{ $consumers_name }}-consumer
    spec:
      topologySpreadConstraints:
      - maxSkew: 1
        topologyKey: kubernetes.io/hostname
        whenUnsatisfiable: ScheduleAnyway
        labelSelector:
          matchLabels:
            app: {{ $consumers_name }}-consumer
      containers:
        - name: {{ $consumers_name }}-consumer
          imagePullPolicy: Always
          image: {{ include "app.image" $top | quote }}
          env:
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  apiVersion: v1
                  fieldPath: metadata.namespace
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  apiVersion: v1
                  fieldPath: metadata.name
            - name: VMB_SERVICE_URL 
              value: {{ include "app.vmb.url" $top }}             
            - name: LOG_APPENDER
              value: {{ include "app.log.appender" $top }}
            - name: LOG_LEVEL
              value: {{ include "app.log.level" $top }} 
            - name: LOG_TRUNCATE
              value: {{ include "app.log.truncate" $top | quote }}
            - name: S3API_SERVICE_URL
              value: {{ include "app.s3api.url" $top }}
            - name: S3OVERRIDEAUTH
              value: {{ include "app.s3OverrideAuth" $top }}
            - name: S3BUCKETKEY
              value: {{ include "app.s3bucketkey" $top}}
            - name: APP_NAME
              value: {{ include "app.name" $top | quote }}
            - name: TOPIC_NAME
              value: {{ $consumers_name }}   
            - name: TOPIC_URL
              value: {{ $topicurl }}
            - name: S3_JSON
              value: {{ $s3json | toJson | quote }}
            - name: S3FULLMESSAGE
              value: {{ $s3fullmessage | quote }}
            - name: SUBSCRIPTION_NAME
              value: {{ $subscription_name }}
            - name: REGION
              value: {{ include "app.region" $top | quote }}
            - name: VMB_TLS_CERT_FILE
              valueFrom:
                configMapKeyRef:
                  name: {{ $top.Values.global.configmap.vmb }}
                  key: VMB_TLS_CERT_FILE
            - name: VMB_TLS_KEY_FILE
              valueFrom:
                configMapKeyRef:
                  name: {{ $top.Values.global.configmap.vmb }}
                  key: VMB_TLS_KEY_FILE
            - name: VMB_TRUST_CERTS_FILE
              valueFrom:
                configMapKeyRef:
                  name: {{ $top.Values.global.configmap.vmb }}
                  key: VMB_TRUST_CERTS_FILE
            - name: APPENV
              valueFrom:
                configMapKeyRef:
                    name: {{ $top.Values.global.configmap.commons }}
                    key: APPENV
            - name: APP_NAME_SUFFIX
              value: {{ $top.Values.app.namesuffix | quote }}
            - name: NRAPPENV
              valueFrom:
                configMapKeyRef:
                  name: {{ $top.Values.global.configmap.commons }}
                  key: NRAPPENV
            - name: NRLICENSEKEY
              valueFrom:
                secretKeyRef:
                  name: {{ $top.Values.global.secret.newrelic }}
                  key: NRLICENSEKEY
            - name: NRLABELS
              valueFrom:
                secretKeyRef:
                  name: {{ $top.Values.global.secret.newrelic }}
                  key: NRLABELS
  
          resources:
{{ if (hasKey (index $top.Values "kube" $datacenter $cluster $env "CONSUMERS" $consumer) "RESOURCES")}}
{{ $resources | indent 12 }}
{{ else }}
{{ $resources_default | indent 12 }}
{{ end }}
          volumeMounts:
          - name: tmp
            mountPath: /tmp
          - name: vmb-certs
            mountPath: "/prod/eclapp/vmb/ssl"
      imagePullSecrets:
       - name: oneartifactory-secret  
      volumes:
        - name: tmp
          emptyDir: {}
        - name: vmb-certs
          secret:
            secretName: {{ $top.Values.global.secret.vmb }}

{{ end }}
{{ end }}
