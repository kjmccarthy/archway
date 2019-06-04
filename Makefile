package-api: 
	sbt "set every test in assembly := {}" api/assembly

package-ui:
	npm run-script prepare

test: 
	sbt test

itest:
	sbt itest

serve-api:
	echo "using TRUST_STORE: $${TRUST_STORE:?}"
	sbt -Djavax.net.ssl.trustStore=$$TRUST_STORE "api/runMain com.heimdali.Server"

serve-ui:
	npm start

init-ui:
	npm install -g typescript
	npm install

validate:
	build-support/bin/validator -p $PWD/cloudera-integration/parcel/heimdali-meta/parcel.json
	build-support/bin/validator -r $PWD/cloudera-integration/parcel/heimdali-meta/permissions.json
	build-support/bin/validator -p $PWD/cloudera-integration/parcel/custom-shell-meta/parcel.json
	build-support/bin/validator -s $PWD/cloudera-integration/csd/descriptor/service.sdl

.PHONY: dist

parcel:
	echo "using HEIMDALI_VERSION: $${HEIMDALI_VERSION:?}"
	./publish.sh parcel heimdali
	./publish.sh manifest

csd:
	echo "using HEIMDALI_VERSION: $${HEIMDALI_VERSION:?}"
	./publish.sh csd

repo: parcel csd

dist: package-api package-ui repo

ship:
	echo "using HEIMDALI_VERSION: $${HEIMDALI_VERSION:?}"
	echo "using DEPLOY_REPO: $${DEPLOY_REPO:?}"
	./publish.sh ship

clean:
	sbt clean 
	rm -rf dist
	rm -rf cloudera-integration/build