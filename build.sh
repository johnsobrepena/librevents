#!/bin/bash

./gradlew build -x :spotlessGroovyGradle -x :core:spotlessGroovyGradle -x :integration-tests:spotlessGroovyGradle -x :core:test -x :privacy:spotlessGroovyGradle -x :integration-tests:test -x :server:spotlessGroovyGradle -x :privacy:test
