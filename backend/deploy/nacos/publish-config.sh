#!/bin/sh
set -eu

publish() {
  data_id="$1"
  file="$2"
  curl --fail --silent --show-error -X POST "http://${NACOS_ADDR}/nacos/v1/cs/configs" \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode "group=REFINE" \
    --data-urlencode "type=json" \
    --data-urlencode "content@${file}"
  echo "Published ${data_id}"
}

until curl --fail --silent "http://${NACOS_ADDR}/nacos/v1/console/health/readiness" >/dev/null; do sleep 2; done
publish refine-gateway-sentinel.json /config/refine-gateway-sentinel.json
publish refine-ai-sentinel.json /config/refine-ai-sentinel.json

