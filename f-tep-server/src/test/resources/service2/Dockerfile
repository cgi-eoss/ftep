FROM alpine:latest

COPY chiapas_ground_plot/* /testdata/

COPY workflow.sh /tmp/workflow.sh
RUN chmod +x /tmp/workflow.sh

ENTRYPOINT ["/tmp/workflow.sh"]
