# Seed

```shell
curl -X POST http://localhost:9000/raw/iou/100
curl -X GET http://localhost:9000/raw/iou/amountOwed/{protocolId}
curl -X POST http://localhost:9000/raw/iou/pay/{protocolId} -d '{"value": "1"}'
curl -X POST http://localhost:9000/raw/iou/forgive/{protocolId}



curl -X POST http://localhost:9000/codegen/iou/100
curl -X GET http://localhost:9000/codegen/iou/amountOwed/{protocolId}
curl -X POST http://localhost:9000/codegen/iou/pay/{protocolId} -d '{"value": "1"}'
curl -X POST http://localhost:9000/codegen/iou/forgive/{protocolId}
```