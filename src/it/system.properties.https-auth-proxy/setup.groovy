def dockerRun = 'docker run --rm -d --name test-auth-proxy -e SQUID_USERNAME=foo -e SQUID_PASSWORD=bar -p 0.0.0.0:3128:3128 robhaswell/squid-authenticated@sha256:6a99946c96d063981b329c22efc2b9ad1ac4e90d16ddcbb9d0b2d6773a7bea2b'.execute()
dockerRun.waitForOrKill(30_000)
