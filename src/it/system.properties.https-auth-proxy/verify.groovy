def logOut = new StringBuilder(), logErr = new StringBuilder()
def dockerLogs = 'docker exec test-auth-proxy cat /opt/squid/access.log'.execute()
dockerLogs.consumeProcessOutput(logOut, logErr)
dockerLogs.waitForOrKill(10_000)
def dockerStop = 'docker stop test-auth-proxy'.execute()
dockerStop.waitForOrKill(30_000)
new FileWriter(new File(basedir, 'access.log')).withWriter { it << logOut }
assert logOut.toString().contains('CONNECT services.gradle.org:443')
def buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('Gradle 8.12 download complete')
assert buildLog.contains('Gradle Tooling API 8.12 download complete')
