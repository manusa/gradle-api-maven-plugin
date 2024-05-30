def dockerRun = 'docker run --rm -d --name test-auth-proxy -e SQUID_USERNAME=foo -e SQUID_PASSWORD=bar -p 0.0.0.0:3128:3128 marcnuri/squid-simple-auth:latest'.execute()
dockerRun.waitForOrKill(30_000)
def count = 10
def ready = false
while(count-- > 0 && !ready) {
  def logOut = new StringBuilder(), logErr = new StringBuilder()
  def dockerLogs = 'docker logs test-auth-proxy'.execute()
  dockerLogs.consumeProcessOutput(logOut, logErr)
  dockerLogs.waitForOrKill(10_000)
  if (logOut.contains('Accepting HTTP Socket connections')) {
    ready = true
  }
  Thread.sleep(1000)
}
