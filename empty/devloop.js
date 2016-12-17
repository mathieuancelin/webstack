'use strict';

let watchApp = run({
  name: 'Java',
  sh: 'echo App changed',
  watch: 'app/**/*'
});

let watchStatic = run({
  name: 'Static',
  sh: 'echo Res changed',
  watch: 'res/**/*'
});

let server = runServer({
  httpPort,
  sh: `SERVER_PORT=${httpPort} ./gradlew run`
}).dependsOn(watchJava, watchStatic);

proxy(server, 9000);