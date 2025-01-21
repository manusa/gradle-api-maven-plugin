MAVEN_OPTIONS=
LICENSE_FILE=license-header.txt

.PHONY: clean
clean:
	./mvnw clean

.PHONY: build
build:
	./mvnw package

.PHONY: test
test:
	./mvnw verify

.PHONY: release
release:
	@if [ -z "$(V)" ]; then echo "V is not set"; exit 1; fi
	@if [ -z "$(VS)" ]; then echo "VS is not set"; exit 1; fi
	@mvn versions:set -DnewVersion=$(V) -DgenerateBackupPoms=false
	@git add .
	@git commit -m "[RELEASE] Updated project version to v$(V)"
	@git tag v$(V)
	@git push origin v$(V)
	@mvn versions:set -DnewVersion=$(VS)-SNAPSHOT -DgenerateBackupPoms=false
	@git add .
	@git commit -m "[RELEASE] v$(V) released, prepare for next development iteration"
	@git push origin main

.PHONY: license
license:
	@license_len=$$(wc -l $(LICENSE_FILE) | cut -f1 -d ' ') &&																\
	 files=$$(find . -type f -name "*.java") &&																								\
	 for file in $$files; do																																	\
	   echo "Applying license to $$file";																											\
	   head -n $$license_len $$file | diff -q $(LICENSE_FILE) - > /dev/null ||								\
	     ( ( cat $(LICENSE_FILE); echo; cat $$file ) > $$file.temp; mv $$file.temp $$file )		\
	 done
