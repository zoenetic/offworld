{
    inputs = {
        nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
        flake-utils.url = "github:numtide/flake-utils";
    };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        jdk = pkgs.jdk25;
        # Native libs the Minecraft client + the native game pull in at runtime.
        runtimeLibs = with pkgs; [
          libGL
          glfw
          openal
          flite
          libpulseaudio
          udev
          libX11
          libXcursor
          libXrandr
          libXxf86vm
          libXi
        ];
      in {
        devShells.default = pkgs.mkShell {
          packages = with pkgs; [
            # --- JVM half (Gradle owns minecraft/**) ---
            jdk
            gradle
            kotlin
            jdt-language-server
            kotlin-language-server

            # --- Rust half (Cargo owns genesis/* + game) ---
            rustc
            cargo
            clippy
            rustfmt
            rust-analyzer

            # --- shared tooling ---
            jq
            ripgrep
            git
            jbang
          ];
          JAVA_HOME = jdk.home;
          LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath runtimeLibs;
          shellHook = ''
            echo "🌍 offworld dev shell — Cargo + Gradle side by side"
            echo "   JDK:    $(java -version 2>&1 | head -1)"
            echo "   Gradle: $(gradle --version | grep Gradle | head -1)"
            echo "   Rust:   $(rustc --version)"
          '';
      };
    });
}
