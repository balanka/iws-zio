From openjdk:26-ea-slim as stage0
LABEL snp-multi-stage="intermediate"
LABEL snp-multi-stage-id="3f994b13-3aec-44c6-ac9f-53e7cbee134f"
WORKDIR /opt/docker
COPY 2/opt /2/opt
COPY 4/opt /4/opt
USER root
RUN ["chmod", "-R", "u=rX,g=rX", "/2/opt/docker"]
RUN ["chmod", "-R", "u=rX,g=rX", "/4/opt/docker"]
RUN ["chmod", "u+x,g+x", "/4/opt/docker/bin/iws-zio"]
RUN ["chmod", "u+x,g+x", "/4/opt/docker/bin/server-sent-event-endpoint"]

From openjdk:26-ea-slim as mainstage
LABEL MAINTAINER="batexy@gmail.com"
USER root
RUN id -u bateka4911 1>/dev/null 2>&1 || (( getent group 0 1>/dev/null 2>&1 || ( type groupadd 1>/dev/null 2>&1 && groupadd -g 0 root || addgroup -g 0 -S root )) && ( type useradd 1>/dev/null 2>&1 && useradd --system --create-home --uid 1001 --gid 0 bateka4911 || adduser -S -u 1001 -G root bateka4911 ))
WORKDIR /opt/docker
COPY --from=stage0 --chown=bateka4911:root /2/opt/docker /opt/docker
COPY --from=stage0 --chown=bateka4911:root /4/opt/docker /opt/docker
USER 1001:0
ENTRYPOINT ["/opt/docker/bin/iws-zio"]
CMD []
