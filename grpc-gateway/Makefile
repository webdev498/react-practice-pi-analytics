APP_NAME=service-address-admin-grpc-gateway
VERSION?=1.0
BUILD?=SNAPSHOT
DOCKER_IMAGE=asia.gcr.io/pi-devops/$(APP_NAME):$(VERSION)-$(BUILD)
KUBE_CONTEXT?=''

docker:
	rm -rf src/grpc-gateway/generated/
	# service-address-admin-backend
	mkdir -p src/grpc-gateway/generated/serviceaddresssortingservice/google/protobuf/
	cp -r ../pi-shared-protobuf/src/google/protobuf/. src/grpc-gateway/generated/serviceaddresssortingservice/google/protobuf/
	cp ../pi-shared-protobuf/src/citation_search.proto src/grpc-gateway/generated/serviceaddresssortingservice/
	cp ../pi-shared-protobuf/src/cite_service.proto src/grpc-gateway/generated/serviceaddresssortingservice/
	cp ../pi-shared-protobuf/src/ip-datastore-common.proto src/grpc-gateway/generated/serviceaddresssortingservice/
	cp ../pi-shared-protobuf/src/ip-datastore-sg3.proto src/grpc-gateway/generated/serviceaddresssortingservice/
	cp ../pi-shared-protobuf/src/patent_common.proto src/grpc-gateway/generated/serviceaddresssortingservice/
	cp ../pi-shared-protobuf/src/queue_common.proto src/grpc-gateway/generated/serviceaddresssortingservice/
	cp ../pi-shared-protobuf/src/queue_onprem.proto src/grpc-gateway/generated/serviceaddresssortingservice/
	cp ../pi-shared-protobuf/src/relational_db_service.proto src/grpc-gateway/generated/serviceaddresssortingservice/
	cp ../pi-shared-protobuf/src/service_address_sorting.proto src/grpc-gateway/generated/serviceaddresssortingservice/
	# Build Docker image
	docker build -t asia.gcr.io/pi-devops/service-address-admin-grpc-gateway:$(VERSION)-$(BUILD) .

dockerpush:
	docker push $(DOCKER_IMAGE)

dockerrmi:
	docker rmi -f $(DOCKER_IMAGE)

deploy:
	kubectl set image deployment/$(APP_NAME) $(APP_NAME)=$(DOCKER_IMAGE) --context=$(KUBE_CONTEXT)
	kubectl rollout status deployment/$(APP_NAME) --context=$(KUBE_CONTEXT)