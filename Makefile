MAVEN_OPTIONS=

.PHONY: clean
clean:
	./mvnw clean

.PHONY: build
build:
	./mvnw package

.PHONY: test
test:
	./mvnw verify
