def proc = 'docker run --rm -d --name test-proxy -p 0.0.0.0:3128:3128 ubuntu/squid:5.2-22.04_beta'.execute()
proc.waitForOrKill(30_000)
def count = 10
def ready = false
while(count-- > 0 && !ready) {
  def logOut = new StringBuilder(), logErr = new StringBuilder()
  def dockerLogs = 'docker logs test-proxy'.execute()
  dockerLogs.consumeProcessOutput(logOut, logErr)
  dockerLogs.waitForOrKill(10_000)
  if (logOut.contains('Accepting HTTP Socket connections')) {
    ready = true
  }
  Thread.sleep(1000)
}
