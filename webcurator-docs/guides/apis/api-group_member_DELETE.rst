Remove Group Member (DELETE)
============================
Remove a target or group from a group.

Request
-------
``https://--WCT_base--/api/v1/groups/{group-id}/members/targets/{target-id}`` OR ``https://--WCT_base--/api/v1/groups/{group-id}/members/groups/{group-id}``

Header
------
.. include:: /guides/apis/descriptions/desc-header-authentication.rst

Body
----
.. include:: /guides/apis/descriptions/desc-request-body-empty.rst

Response
--------
201: OK

.. include:: /guides/apis/descriptions/desc-response-body-empty.rst

Errors
------
If any error is raised no output is returned. Nor is the group created.

=== ==========================================================================
400 Bad request, non-existing target-id has been given.
400 Bad request, non-existing part has been given.
403 Not authorized, user is no longer logged in.
405 Method not allowed, only POST, GET, PUT, DELETE are allowed.
=== ==========================================================================

Example
-------
.. code-block:: linux

  TODO