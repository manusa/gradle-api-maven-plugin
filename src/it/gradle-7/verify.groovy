def buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('Extracting Gradle 7.')
