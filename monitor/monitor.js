const { spawn, execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const http = require('http');
const os = require('os');
const net = require('net');

const PORT = 9999;
const PROJECT_ROOT = path.resolve(__dirname, '..');

const logs = {
    mysql: [],
    backend: [],
    frontend: [],
    system: []
};

const status = {
    mysql: 'stopped',
    backend: 'stopped',
    frontend: 'stopped',
    cpu: '0%',
    memory: '0%'
};

const maxLogSize = 100;

function checkPort(port, host = '127.0.0.1') {
    return new Promise((resolve) => {
        const socket = new net.Socket();
        socket.setTimeout(1000);
        socket.on('connect', () => {
            socket.destroy();
            resolve(true);
        });
        socket.on('timeout', () => {
            socket.destroy();
            resolve(false);
        });
        socket.on('error', () => {
            resolve(false);
        });
        socket.connect(port, host);
    });
}

function stripAnsi(str) {
    return str.replace(/\x1B\[[\d;]*[a-zA-Z]/g, '').trim();
}

function addLog(service, type, message) {
    const timestamp = new Date().toLocaleTimeString();
    logs[service].push({ timestamp, type, message });
    if (logs[service].length > maxLogSize) {
        logs[service].shift();
    }
    broadcast({ type: 'log', service, log: { timestamp, type, message } });
}

function updateStatus(service, newStatus) {
    status[service] = newStatus;
    broadcast({ type: 'status', service, status: newStatus });
}

let clients = [];

function broadcast(data) {
    clients.forEach(client => {
        try {
            client.res.write(`data: ${JSON.stringify(data)}\n\n`);
        } catch (e) { }
    });
}

const procs = [];

function startMySQL() {
    updateStatus('mysql', 'starting');
    addLog('system', 'info', 'Starting MySQL...');
    
    const mysqlDir = path.join(PROJECT_ROOT, 'runtime', 'mysql');
    const mysqldPath = path.join(mysqlDir, 'bin', 'mysqld.exe');
    
    if (!fs.existsSync(mysqldPath)) {
        addLog('mysql', 'error', 'mysqld.exe not found at ' + mysqldPath);
        updateStatus('mysql', 'error');
        return;
    }

    const mysqlProc = spawn(mysqldPath, ['--defaults-file=my.ini', '--console'], {
        cwd: mysqlDir,
        shell: true
    });
    procs.push(mysqlProc);

    mysqlProc.stdout.on('data', (data) => {
        const msg = stripAnsi(data.toString());
        if(msg) addLog('mysql', 'info', msg);
        if (msg.toLowerCase().includes('ready for connections')) {
            updateStatus('mysql', 'running');
        }
    });

    mysqlProc.stderr.on('data', (data) => {
        const msg = stripAnsi(data.toString());
        if(msg) {
            if (msg.toLowerCase().includes('error')) {
                addLog('mysql', 'error', msg);
                updateStatus('mysql', 'error');
            } else {
                addLog('mysql', 'info', msg);
                if (msg.toLowerCase().includes('ready for connections')) {
                    updateStatus('mysql', 'running');
                }
            }
        }
    });

    mysqlProc.on('close', (code) => {
        addLog('mysql', 'error', `MySQL exited with code ${code}`);
        updateStatus('mysql', 'stopped');
    });
}

function startBackend() {
    updateStatus('backend', 'starting');
    addLog('system', 'info', 'Starting Backend...');
    
    const backendDir = path.join(PROJECT_ROOT, 'backend');
    const javaPath = path.join(PROJECT_ROOT, 'runtime', 'java', 'bin', 'java.exe');
    const jarPath = path.join(backendDir, 'target', 'exam-system-1.0.0.jar');
    
    if (!fs.existsSync(javaPath)) {
        addLog('backend', 'error', 'java.exe not found at ' + javaPath);
        updateStatus('backend', 'error');
        return;
    }

    if (!fs.existsSync(jarPath)) {
        addLog('backend', 'error', 'jar file not found at ' + jarPath);
        addLog('backend', 'info', 'Please run maven build first.');
        updateStatus('backend', 'error');
        return;
    }

    const backendProc = spawn(javaPath, ['-Dfile.encoding=UTF-8', '-jar', 'target\\exam-system-1.0.0.jar'], {
        cwd: backendDir,
        shell: true
    });
    procs.push(backendProc);

    backendProc.stdout.on('data', (data) => {
        const msg = stripAnsi(data.toString());
        if(msg) {
            addLog('backend', 'info', msg);
            if (msg.includes('Started') && msg.includes('JVM running for')) {
                updateStatus('backend', 'running');
            }
        }
    });

    backendProc.stderr.on('data', (data) => {
        const msg = stripAnsi(data.toString());
        if(msg) {
            addLog('backend', 'error', msg);
            updateStatus('backend', 'error');
        }
    });

    backendProc.on('close', (code) => {
        addLog('backend', 'error', `Backend exited with code ${code}`);
        updateStatus('backend', 'stopped');
    });
}

function startFrontend() {
    updateStatus('frontend', 'starting');
    addLog('system', 'info', 'Starting Frontend...');
    
    const frontendDir = path.join(PROJECT_ROOT, 'frontend');
    
    // Check if nodejs runtime exists
    let nodePath = path.join(PROJECT_ROOT, 'runtime', 'nodejs', 'npm.cmd');
    let useGlobalNode = false;
    if (!fs.existsSync(nodePath)) {
        addLog('system', 'info', 'Local nodejs not found, falling back to global npm');
        nodePath = 'npm';
        useGlobalNode = true;
    }

    const frontendProc = spawn(nodePath, ['run', 'dev'], {
        cwd: frontendDir,
        shell: true,
        env: { ...process.env, PATH: useGlobalNode ? process.env.PATH : `${path.join(PROJECT_ROOT, 'runtime', 'nodejs')};${process.env.PATH}` }
    });
    procs.push(frontendProc);

    frontendProc.stdout.on('data', (data) => {
        const msg = stripAnsi(data.toString());
        if(msg) {
            addLog('frontend', 'info', msg);
            if (msg.includes('Local')) {
                updateStatus('frontend', 'running');
            }
        }
    });

    frontendProc.stderr.on('data', (data) => {
        const msg = stripAnsi(data.toString());
        if(msg) {
            // Some vite info goes to stderr, check for actual errors
            if (msg.toLowerCase().includes('error')) {
                addLog('frontend', 'error', msg);
                updateStatus('frontend', 'error');
            } else {
                addLog('frontend', 'info', msg);
            }
        }
    });

    frontendProc.on('close', (code) => {
        addLog('frontend', 'error', `Frontend exited with code ${code}`);
        updateStatus('frontend', 'stopped');
    });
}

// OS metrics and Status Check
setInterval(async () => {
    const cpus = os.cpus();
    let user = 0, nice = 0, sys = 0, idle = 0, irq = 0;
    for (let cpu in cpus) {
        user += cpus[cpu].times.user;
        nice += cpus[cpu].times.nice;
        sys += cpus[cpu].times.sys;
        irq += cpus[cpu].times.irq;
        idle += cpus[cpu].times.idle;
    }
    const total = user + nice + sys + idle + irq;
    const currentCpu = ((total - idle) / total) * 100;
    
    const totalMem = os.totalmem();
    const freeMem = os.freemem();
    const usedMem = ((totalMem - freeMem) / totalMem) * 100;
    
    status.cpu = currentCpu.toFixed(1) + '%';
    status.memory = usedMem.toFixed(1) + '%';
    
    broadcast({ type: 'system_metrics', cpu: status.cpu, memory: status.memory });

    // Check ports for actual running status
    const mysqlRunning = await checkPort(3307);
    if (mysqlRunning && status.mysql !== 'running') {
        updateStatus('mysql', 'running');
    }

    const backendRunning = await checkPort(8080);
    if (backendRunning && status.backend !== 'running') {
        updateStatus('backend', 'running');
    }

    const frontendRunning = await checkPort(5173);
    if (frontendRunning && status.frontend !== 'running') {
        updateStatus('frontend', 'running');
    }
}, 2000);

// HTTP Server
const server = http.createServer((req, res) => {
    if (req.url === '/') {
        fs.readFile(path.join(__dirname, 'index.html'), (err, data) => {
            if (err) {
                res.writeHead(500);
                res.end('Error loading index.html');
                return;
            }
            res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
            res.end(data);
        });
    } else if (req.url === '/api/state') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status, logs }));
    } else if (req.url === '/api/stream') {
        res.writeHead(200, {
            'Content-Type': 'text/event-stream',
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive'
        });
        
        const client = { res };
        clients.push(client);
        
        req.on('close', () => {
            clients = clients.filter(c => c !== client);
        });
    } else if (req.url === '/api/start') {
        if (status.mysql === 'stopped') startMySQL();
        setTimeout(() => {
            if (status.backend === 'stopped') startBackend();
        }, 3000);
        setTimeout(() => {
            if (status.frontend === 'stopped') startFrontend();
        }, 5000);
        res.writeHead(200);
        res.end('Started');
    } else {
        res.writeHead(404);
        res.end();
    }
});

server.listen(PORT, () => {
    console.log(`Monitoring center started on http://localhost:${PORT}`);
    const startUrl = `http://localhost:${PORT}`;
    const startCmd = process.platform === 'win32' ? 'start' : (process.platform === 'darwin' ? 'open' : 'xdg-open');
    execSync(`${startCmd} ${startUrl}`);
});

function cleanup() {
    console.log('Cleaning up processes...');
    procs.forEach(p => {
        try {
            if (process.platform === 'win32') {
                execSync(`taskkill /pid ${p.pid} /T /F`);
            } else {
                p.kill();
            }
        } catch (e) { }
    });
    process.exit();
}

process.on('SIGINT', cleanup);
process.on('SIGTERM', cleanup);
process.on('exit', cleanup);
