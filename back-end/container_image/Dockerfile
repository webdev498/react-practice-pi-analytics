FROM asia.gcr.io/pi-devops/oracle-java8:8u131

EXPOSE 8080

ADD service-address-admin-backend-latest.tar /
RUN chmod +x /service-address-admin-backend-latest/bin/service-address-admin-backend

# default command to start full server
CMD ["/service-address-admin-latest/bin/service-address-admin-backend"]
