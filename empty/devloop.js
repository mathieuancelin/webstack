'use strict';

let compileJava = run({
  name: 'java',
  sh: './gradlew compileJava',
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
}).dependsOn(compileJava, watchStatic);

proxy(server, 9000);