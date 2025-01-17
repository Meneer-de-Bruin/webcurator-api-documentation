============
API Overview
============
In general the version number in the api call is the letter 'v' followed by the latest major version number. E.g. 'v1'. It is also possible 
to use the string 'latest' to call upon the latest version of the api. 

If an api is deprecated and no longer supported then the error 404 'Not Found – the requested resource does not exist' is returned. 

Agency
======
.. toctree::
   :maxdepth: 1

   api-agencies_GET

Authentication
==============
.. toctree::
   :maxdepth: 1

   api-authentication_POST
   api-authentication_DELETE

Flag
====
.. toctree::
   :maxdepth: 1
   
   api-flags_GET.rst

Group
=====
.. toctree::
   :maxdepth: 1
   
   api-groups_GET.rst
   api-group_GET.rst
   api-group_DELETE.rst
   api-group_states_GET.rst
   api-group_types_GET.rst

Harvest Agents
==============
.. toctree::
   :maxdepth: 1
   
   api-harvest-agents_GET.rst
   
Harvest Authorisations
======================
.. toctree::
   :maxdepth: 1
   
   api-harvest_authorisations_GET.rst
   api-harvest_authorisation_states_GET.rst
   
Profile
=======
.. toctree::
   :maxdepth: 1
   
   api-profiles_GET.rst
   api-profile_states_GET.rst
   
Targets
=======
.. toctree::
   :maxdepth: 1

   api-targets_GET
   api-target_POST
   api-target_GET
   api-target_PUT
   api-target_DELETE
   api-target_states_GET
   api-target_scheduleTypes_GET
   
Target Instances
================
.. toctree::
   :maxdepth: 1

   api-target_instances_GET
   api-target_instance_GET
   api-target_instance_PUT
   api-target_instance_PUT_abort
   api-target_instance_PUT_patch-harvest
   api-target_instance_PUT_pause
   api-target_instance_PUT_resume
   api-target_instance_PUT_start
   api-target_instance_PUT_stop
   api-target_instance_DELETE
   api-target_instance_states_GET
   api-harvest_result_states_GET

   
User
====
.. toctree::
   :maxdepth: 1
   
   api-users_GET.rst