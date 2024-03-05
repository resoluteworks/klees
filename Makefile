export OP_ACCOUNT := my.1password.com

include .env
export

include gradle.properties

env:
	rm -f .env
	op read "op://Development/klees.env/.env.local" > .env

test:
	./gradlew clean test
	./gradlew coverallsJacoco

publish: test
	./gradlew publish -PpublishToMavenCentral=true

publish-local: test
	./gradlew publish -PpublishToMavenCentral=false

release: publish
	@echo $(kleesVersion)
	git tag "v$(kleesVersion)" -m "Release v$(kleesVersion)"
	git push --tags --force
	@echo Finished building version $(kleesVersion)
