const { exec } = require('child_process');
const http = require('http');
const os = require('os');

// Check ports in reverse order since Next.js uses first available starting from 3000
// So if 3000, 3001 are in use, Next.js will be on 3002
const ports = [3004, 3003, 3002, 3001, 3000];
const maxAttempts = 30; // Try for 30 seconds

function checkPort(port) {
  return new Promise((resolve) => {
    const req = http.get(`http://localhost:${port}`, (res) => {
      // Check if it's likely a Next.js server
      // Next.js typically responds with 200, 404, or other status codes
      // We check for the presence of common Next.js headers or valid HTTP response
      const isNextJs = res.statusCode >= 200 && res.statusCode < 600;
      const hasNextJsHeaders = res.headers['x-powered-by'] === 'Next.js' || 
                                res.headers['content-type']?.includes('text/html') ||
                                res.headers['content-type']?.includes('application/json');
      
      if (isNextJs && (hasNextJsHeaders || res.statusCode === 200 || res.statusCode === 404)) {
        resolve(true);
      } else {
        resolve(false);
      }
      res.resume(); // Consume the response to free up memory
    });
    req.on('error', () => {
      resolve(false);
    });
    req.setTimeout(1000, () => {
      req.destroy();
      resolve(false);
    });
  });
}

function openBrowser(url) {
  const platform = os.platform();
  let command;

  if (platform === 'win32') {
    // Use PowerShell Start-Process which is more reliable on Windows
    command = `powershell -Command "Start-Process '${url}'"`;
  } else if (platform === 'darwin') {
    command = `open "${url}"`;
  } else {
    command = `xdg-open "${url}"`;
  }

  exec(command, (error) => {
    if (error) {
      console.error(`Failed to open browser: ${error.message}`);
      // Fallback: try using the default browser directly
      if (platform === 'win32') {
        exec(`start "" "${url}"`, () => {});
      }
    }
  });
}

async function findAndOpenPort() {
  for (let i = 0; i < maxAttempts; i++) {
    // Check ports in reverse order to find the Next.js server first
    // (Next.js uses first available starting from 3000, so if 3000-3001 are in use, it's on 3002)
    for (const port of ports) {
      const isAvailable = await checkPort(port);
      if (isAvailable) {
        console.log(`\n✓ Server found on port ${port}`);
        console.log(`Opening http://localhost:${port} in browser...\n`);
        openBrowser(`http://localhost:${port}`);
        return;
      }
    }
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  console.log('\n⚠ Could not detect server port. Please open manually.\n');
}

// Handle uncaught errors gracefully
process.on('unhandledRejection', (error) => {
  // Silently handle - browser opening is not critical
});

findAndOpenPort();

