'use strict';

let watchJava = run({
  name: 'Java',
  sh: 'echo Java changed',
  watch: 'src/main/java/**/*.java'
});

let watchStatic = run({
  name: 'Static',
  sh: 'echo Static changed',
  watch: 'src/main/resources/**/*'
});

let server = runServer({
  httpPort,
  sh: `SERVER_PORT=${httpPort} ./gradlew run`
}).dependsOn(watchJava, watchStatic);

proxy(server, 9000);