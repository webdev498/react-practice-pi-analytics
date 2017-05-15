APP_NAME=service-address-admin-frontend
VERSION?=1.0
BUILD?=SNAPSHOT
DOCKER_IMAGE=asia.gcr.io/pi-devops/$(APP_NAME):$(VERSION)-$(BUILD)
KUBE_CONTEXT?=''
KUBE_NAMESPACE?='onprem-prod'

docker: clean
	mkdir -p build-artifacts/protobuf-src/
	cp -r ../pi-shared-protobuf/src/. build-artifacts/protobuf-src/
	docker build -t $(DOCKER_IMAGE) .

dockerpush:
	docker push $(DOCKER_IMAGE)

dockerrmi:
	docker rmi -f $(DOCKER_IMAGE)

deploy:
	kubectl set image deployment/$(APP_NAME) $(APP_NAME)=$(DOCKER_IMAGE) --context=$(KUBE_CONTEXT) --namespace=$(KUBE_NAMESPACE)
	kubectl rollout status deployment/$(APP_NAME) --context=$(KUBE_CONTEXT) --namespace=$(KUBE_NAMESPACE)

test:
	cd src; npm install; npm test

clean:
	rm -rf build-artifacts/

