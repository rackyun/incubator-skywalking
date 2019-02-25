#!/usr/bin/env bash
git submodule init
git submodule update
./mvnw clean package -Dmaven.test.skip=true $1
tar -zcf agent.tar.gz skywalking-agent