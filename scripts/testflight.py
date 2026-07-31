#!/usr/bin/env python3
"""The TestFlight automation steps for CI.

Commands:
  check-age      Exits with code 3 when the newest build has more than 35 days of
                 life. Then the workflow stops, and no new build is necessary.
  finish         Waits until the newest build is processed, sets the export
                 compliance, attaches the build to the public group, and submits it
                 for beta review.

The credentials come from the environment: ASC_KEY_ID, ASC_ISSUER_ID, ASC_APP_ID,
ASC_GROUP_ID, and the key file at ~/.appstoreconnect/private_keys/.
"""
import datetime
import json
import os
import sys
import time
import urllib.error
import urllib.request

import jwt

BASE = "https://api.appstoreconnect.apple.com/v1"
REFRESH_BEFORE_DAYS = 35


def token():
    key_id = os.environ["ASC_KEY_ID"]
    with open(os.path.expanduser(f"~/.appstoreconnect/private_keys/AuthKey_{key_id}.p8")) as f:
        pk = f.read()
    now = int(time.time())
    return jwt.encode(
        {"iss": os.environ["ASC_ISSUER_ID"], "iat": now, "exp": now + 900,
         "aud": "appstoreconnect-v1"},
        pk, algorithm="ES256", headers={"kid": key_id, "typ": "JWT"})


def call(method, path, body=None, ok_codes=()):
    req = urllib.request.Request(
        BASE + path, method=method,
        headers={"Authorization": f"Bearer {token()}", "Content-Type": "application/json"},
        data=json.dumps(body).encode() if body else None)
    try:
        with urllib.request.urlopen(req) as r:
            return json.load(r) if r.status != 204 else {}
    except urllib.error.HTTPError as e:
        if e.code in ok_codes:
            print(f"HTTP {e.code} accepted for {path}")
            return {}
        print(f"HTTP {e.code}: {e.read().decode()[:400]}", file=sys.stderr)
        raise SystemExit(1)


def newest_build():
    app = os.environ["ASC_APP_ID"]
    builds = call("GET", f"/builds?filter[app]={app}&sort=-uploadedDate&limit=1")
    return builds["data"][0] if builds["data"] else None


def check_age():
    build = newest_build()
    if build is None:
        print("no builds; a new build is necessary")
        return
    expires = datetime.datetime.fromisoformat(build["attributes"]["expirationDate"])
    days = (expires - datetime.datetime.now(datetime.timezone.utc)).days
    print(f"newest build expires {expires:%Y-%m-%d}; {days} days remain")
    if days > REFRESH_BEFORE_DAYS:
        print("the build is fresh; no upload is necessary")
        sys.exit(3)
    print("the build is near its end; a new build is necessary")


def finish():
    # The upload from xcodebuild completed just before this step. Wait for the
    # processed state of the newest build.
    build = None
    for _ in range(60):
        build = newest_build()
        state = build["attributes"]["processingState"] if build else "NONE"
        print("processing state:", state)
        if state == "VALID":
            break
        if state in ("FAILED", "INVALID"):
            sys.exit("the build did not process")
        time.sleep(30)
    else:
        sys.exit("the build did not process in time")

    build_id = build["id"]
    call("PATCH", f"/builds/{build_id}", {"data": {
        "type": "builds", "id": build_id,
        "attributes": {"usesNonExemptEncryption": False}}})
    print("compliance set")

    call("POST", f"/betaGroups/{os.environ['ASC_GROUP_ID']}/relationships/builds",
         {"data": [{"type": "builds", "id": build_id}]}, ok_codes=(409,))
    print("build attached to the public group")

    call("POST", "/betaAppReviewSubmissions", {"data": {
        "type": "betaAppReviewSubmissions",
        "relationships": {"build": {"data": {"type": "builds", "id": build_id}}}}},
        ok_codes=(409, 422))
    print("beta review submitted")


if __name__ == "__main__":
    {"check-age": check_age, "finish": finish}[sys.argv[1]]()
