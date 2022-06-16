# Recipes

This is a collection of device pool recipes that can be used for both devices
and applications.

## Device Lab Bootstrap

This recipe is largely just a shallow copy of the [example infra][1]. Spin up your
own infrastructure with relative ease, and start playing around.

[1]: ../device-pool-examples/device-pool-examples-infra/README.md

## Device Registration and Health

This recipe is used device that belong to `MANAGED` pools. In place of having
membership be dynamic, you can use agent information to update devices.

```shell
bash <(curl -L https://raw.githubusercontent.com/philcali/device-pool/main/recipes/install.sh) -t device-registration -w
```