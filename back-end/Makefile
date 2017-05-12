dist:
	./gradlew clean build publish -x test -x check distTar

localdeploy: dist
	rm -f container_image/service-address-admin-backend-latest.tar
	mv build/distributions/service-address-admin-backend-latest.tar container_image/service-address-admin-backend-latest.tar

clean:
	./gradlew clean

test:
	./gradlew clean build check test

