# NOTE: This file is auto generated by OpenAPI Generator 7.0.0-SNAPSHOT (https://openapi-generator.tech).
# Do not edit this file manually.

defmodule OpenapiPetstore.Model.FileSchemaTestClass do
  @moduledoc """
  
  """

  @derive Jason.Encoder
  defstruct [
    :file,
    :files
  ]

  @type t :: %__MODULE__{
    :file => OpenapiPetstore.Model.File.t | nil,
    :files => [OpenapiPetstore.Model.File.t] | nil
  }
end

