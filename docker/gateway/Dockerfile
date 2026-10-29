FROM nginxinc/nginx-unprivileged:stable-alpine@sha256:f5116ed4606f3eb015dd4a8e28cb9b351476dc0ad4bc6e9f93b19cec130cdf03
COPY default.conf.template /etc/nginx/templates/default.conf.template
USER 101:101
EXPOSE 8080
