#!/usr/bin/env python3
"""JDK21 + disposable independent MySQL; never reads .env/development config."""
import os, pathlib, subprocess, time, uuid, sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=pathlib.Path('/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home')
def run(args,**kw): return subprocess.run(args,check=True,**kw)
name='blog-ownership-'+uuid.uuid4().hex[:12]
if not (JAVA/'bin/java').exists():sys.exit('Install Homebrew openjdk@21 first')
env={k:v for k,v in os.environ.items() if not k.startswith(('SPRING_','BLOG_','MAIL_','JAVA_','JDK_','MAVEN_','LOGGING_')) and k not in ('_JAVA_OPTIONS','DEBUG')}
env.update(JAVA_HOME=str(JAVA),PATH=str(JAVA/'bin')+':'+os.environ.get('PATH',''))
try:
    run(['docker','run','-d','--name',name,'--label','blog.validation=ownership',
         '--tmpfs','/var/lib/mysql:rw','-p','127.0.0.1::3306',
         '-e','MYSQL_ROOT_PASSWORD=ownership-test-only','-e','MYSQL_DATABASE=blog_ownership_test',
         'mysql:8.0'],stdout=subprocess.DEVNULL)
    for _ in range(90):
        p=subprocess.run(['docker','exec',name,'mysqladmin','ping','-h','127.0.0.1','-pownership-test-only','--silent'],capture_output=True)
        if p.returncode==0:break
        time.sleep(1)
    else:raise RuntimeError('Isolated MySQL failed to become ready')
    port=subprocess.check_output(['docker','port',name,'3306/tcp'],text=True).strip().rsplit(':',1)[1]
    env.update(BLOG_TEST_PORT=port,BLOG_TEST_GUARD='isolated-blog-ownership')
    run([str(JAVA/'bin/java'),'-version'],env=env)
    run(['mvn','-version'],env=env)
    ROOT.joinpath('target').mkdir(exist_ok=True)
    label=sys.argv[1] if len(sys.argv)>1 else 'validation'
    if not label.replace('-','').isalnum():raise ValueError('Invalid log label')
    log=ROOT/'target'/('ownership-'+label+'.log')
    build=ROOT/'target'/('validation-build-'+name)
    with log.open('w') as out:
        result=subprocess.run(['mvn','--batch-mode','-Pisolated-validation',
                               '-Dvalidation.build.directory='+str(build),'test'],cwd=ROOT,env=env,stdout=out,stderr=subprocess.STDOUT)
    print('Isolated MySQL:',name,'port:',port,'Maven exit:',result.returncode,'log:',log,flush=True)
    print('Isolated build and Surefire reports:',build,flush=True)
    sys.exit(result.returncode)
finally:
    # Exact per-run container; tmpfs only, never shared/development volumes.
    subprocess.run(['docker','stop',name],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    subprocess.run(['docker','rm',name],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
