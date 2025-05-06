# ⚡ JVM-Based Multi-Language Scripting Engine (JavaScript + Python)

A powerful, secure, and fully JVM-native **scripting engine** built using **Spring Boot** and **GraalVM**, supporting dynamic execution of **JavaScript** and **Python** code — including the ability to import and execute third-party libraries like `lodash (javascript)` or `termcolor (python)`, all without external dependencies or `ProcessBuilder`.

---

## ✅ Key Features

### 🔁 Multi-Language Support
- Supports **JavaScript** and **Python**.
- Executes scripts via **GraalVM Polyglot Engine**, fully embedded within the JVM.

### ✍️ Flexible Script Input
- Accepts:
    - Script as a **raw string**
    - Script as a **file upload**
- Automatically detects and executes based on script language.

### 📦 Dependency Extraction and Linking (Key Innovation)
- **Dependency Extractor**: Parses and identifies third-party packages from the script.
- **Dependency Downloader**:
    - Resolves packages from **PyPI** or **Node Registry**.
    - Downloads compressed archives of dependencies.
- **Auto Extraction**:
    - Unzips downloaded `.tgz`, `.whl`, or `.zip` files into a virtual `js/node_modules` or `python` directory.
    - Deletes the archive after extraction to save disk space.

### ⚡ Intelligent Caching
- Avoids redundant downloads — checks if the dependency already exists.
- Works across executions and instances.

### 📂 Virtual File System + Import Mapping
- Automatically maps downloaded modules to proper `import` paths.
- Supports bare imports via virtual folder structure (`node_modules/` and `.py-deps/`).
- Fully compatible with commonJs, `require()` is supported.

### ☁️ Secure & JVM-Native
- Runs **inside the JVM** — no Node.js, Python, Docker, or OS dependencies.
- **No ProcessBuilder or external execution**.
- Future-ready with built-in memory and execution limits.

---

## 🧱 Architectural Overview

```text
┌────────────────────────────────────────┐
│            Spring Boot REST API        │
│  (Accepts Script + Language + File)    │
└────────────────────────────────────────┘
              │
              ▼
┌────────────────────────────────────────┐
│        Language Dispatcher Service     │
│  Routes script to JS or Python Engine  │
└────────────────────────────────────────┘
              │
              ▼
┌────────────────────────────────────────┐
│       GraalVM Polyglot Executor        │
│  Creates polyglot context + bindings   │
└────────────────────────────────────────┘
              │
              ▼
┌────────────────────────────────────────┐
│   Dependency Resolver & Downloader     │
│ Parses script → Extracts dependencies  │
│ Downloads + Extracts + Links to VFS    │
└────────────────────────────────────────┘
              │
              ▼
┌────────────────────────────────────────┐
│       Virtual FileSystem Mapper        │
│ Mounts node_modules or .py-deps        │
│ Rewrites paths for execution           │
└────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Component              | Technology                     | Purpose                                      |
|------------------------|--------------------------------|----------------------------------------------|
| **Framework**          | Spring Boot                    | REST API and application backbone            |
| **Scripting Engine**   | GraalVM Polyglot               | Executes JavaScript and Python scripts       |
| **Dependency Resolver**| Custom Parser + HTTP Client    | Identifies and downloads dependencies        |
| **Build Tool**         | Maven                          | Dependency management and build automation   |
| **Runtime**            | JVM (Java 21)                  | Secure, native execution environment         |

---

## 📡 API Endpoints

Below are the primary API endpoints for interacting with the scripting engine:

### 1. Execute Script - javascript
- **Endpoint**: `POST /api/scripts/execute`
- **Description**: Executes a script provided as a raw string.
- **Request Body**:
  ```json
  {
    "language": "js",
    "script": "const _ = require('lodash'); console.log(_.sum([1, 2, 3, 4]))"
  }
  ```
- **Response**:
  ```json
  {
    "output": "[Module]",
    "success": true
  }
  ```
### 2. Execute Script - python
- **Endpoint**: `POST /api/scripts/execute`
- **Description**: Executes a script provided as a raw string.
- **Request Body**:
  ```json
  {
    "language": "python",
    "script": "from termcolor import colored; print(colored('Hello World', 'red'))"
  }
  ```
- **Response**:
  ```json
  {
    "output": "<module '__main__'>",
    "success": true
  }
  ```

---

## 🚀 Future Enhancements

The following enhancements are planned to further improve the scripting engine:

1. **Proper Dependency Manager**:
    - Fully automate third-party dependency resolution for both Python and JS.
    - Parse package.json, requirements.txt, or inline comments to extract versions.
    - Use semver, caching, and lock files for consistent builds.

2. **Transitive Dependency Support**:
    - Automatically fetch nested dependencies (e.g., axios → follow-redirects).
    - Dependency graph resolver.

3. **Full ES6 Module Compatibility**:
    - Support import maps, aliases, and bare imports with proper path rewrites.

4. **Runtime & Execution Limits**:
    - Timeout support per script (e.g., 5s max).
    - Memory usage limits (e.g., 256MB).
    - Infinite loop protection.

5. **Sandboxing & Permissions**:
    - Restrict access to dangerous global variables or network calls.
    - Define allow/deny lists per language.
