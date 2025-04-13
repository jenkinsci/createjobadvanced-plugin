ARG BASE_IMAGE=ubuntu
FROM ${BASE_IMAGE}
ARG MAVEN_BINARY=https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
ARG USER_CI=jenkinsci
# Install required plugin development package
RUN \
  apt-get update && apt-get install -y git openjdk-21-jdk-headless sudo curl
# Add maven,
RUN \
  mkdir -p /opt/maven && \
  curl -sSLf ${MAVEN_BINARY} | tar -C /opt/maven --strip-components=1 -xzvf -
# Append binary to PATH
ENV PATH=/opt/maven/bin:${PATH}
# Create CI USer
RUN groupadd -r ${USER_CI} && useradd -rm -s /bin/bash -g ${USER_CI} ${USER_CI}
# Turn it sudoer
COPY <<EOT /etc/sudoers.d/${USER_CI}
${USER_CI} ALL=(root) NOPASSWD:ALL
EOT
# Activate user
USER ${USER_CI}
# Add cool prompt
RUN <<EOT
cat <<"EOF" >> /home/${USER_CI}/.bashrc
# bash theme - partly inspired by https://github.com/ohmyzsh/ohmyzsh/blob/master/themes/robbyrussell.zsh-theme
__bash_prompt() {
    local userpart='`export XIT=$? \
        && [ ! -z "${GITHUB_USER:-}" ] && echo -n "\[\033[0;32m\]@${GITHUB_USER:-} " || echo -n "\[\033[0;32m\]\u " \
        && [ "$XIT" -ne "0" ] && echo -n "\[\033[1;31m\]➜" || echo -n "\[\033[0m\]➜"`'
    local gitbranch='`\
        if [ "$(git config --get devcontainers-theme.hide-status 2>/dev/null)" != 1 ] && [ "$(git config --get codespaces-theme.hide-status 2>/dev/null)" != 1 ]; then \
            export BRANCH="$(git --no-optional-locks symbolic-ref --short HEAD 2>/dev/null || git --no-optional-locks rev-parse --short HEAD 2>/dev/null)"; \
            if [ "${BRANCH:-}" != "" ]; then \
                echo -n "\[\033[0;36m\](\[\033[1;31m\]${BRANCH:-}" \
                && if [ "$(git config --get devcontainers-theme.show-dirty 2>/dev/null)" = 1 ] && \
                    git --no-optional-locks ls-files --error-unmatch -m --directory --no-empty-directory -o --exclude-standard ":/*" > /dev/null 2>&1; then \
                        echo -n " \[\033[1;33m\]✗"; \
                fi \
                && echo -n "\[\033[0;36m\]) "; \
            fi; \
        fi`'
    local lightblue='\[\033[1;34m\]'
    local removecolor='\[\033[0m\]'
    PS1="${userpart} ${lightblue}\w ${gitbranch}${removecolor}\$ "
    unset -f __bash_prompt
}
__bash_prompt
EOF
EOT
# Add maven settings for jenkins ci
RUN mkdir -p /home/${USER_CI}/.m2
COPY resources/settings.xml /${USER_CI}/.m2
