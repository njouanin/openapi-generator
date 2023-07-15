# NOTE: This file is auto generated by OpenAPI Generator 7.0.0-SNAPSHOT (https://openapi-generator.tech).
# Do not edit this file manually.

defmodule OpenapiPetstore.Model.ObjectWithDeprecatedFields do
  @moduledoc """
  
  """

  @derive Jason.Encoder
  defstruct [
    :uuid,
    :id,
    :deprecated_ref,
    :bars
  ]

  @type t :: %__MODULE__{
    :uuid => String.t | nil,
    :id => float() | nil,
    :deprecated_ref => OpenapiPetstore.Model.DeprecatedObject.t | nil,
    :bars => [OpenapiPetstore.Model.String.t] | nil
  }
end

