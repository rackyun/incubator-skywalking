#!/usr/bin/env bash
git submodule init
git submodule update
./mvnw clean package -Dmaven.test.skip=true $1
git rev-parse --short HEAD > skywalking-agent/VERSION
cd skywalking-agent
tar -cf ../skywalking-agent.tar *